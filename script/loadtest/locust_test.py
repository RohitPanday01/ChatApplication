import json
import time
import random
import uuid
from locust import HttpUser, task, between, events
import websocket
import gevent

class ChatUser(HttpUser):
    # Simulate realistic human pacing: wait 1 to 3 seconds between actions
    wait_time = between(1, 3)

    def on_start(self):
        """
        Runs automatically when a virtual user connects.
        Establishes a persistent WebSocket connection to your Spring Boot Monolith.
        """

        random_id = random.randint(10000, 99999)

        self.username = f"user_{random_id}"
        self.email = f"user_{random_id}@example.com"
        self.password = "password123"
        self.fullname = f"User {random_id}"
        self.access_token: str | None = None
        self.refresh_token: str | None = None
        self.ws = None
        self.running = True


        response = self.client.post("/api/v1/auth/Signup", json={
            "username": self.username,
            "email": self.email,
            "password": self.password,
            "fullname": self.fullname
        })

        
        if response.status_code not in [200, 204]:
            events.request.fire(
                request_type="HTTP",
                name="User Signup Failure",
                response_time=0,
                response_length=0,
                exception=Exception(f"Signup failed with status code {response.status_code} for {self.username}")
            )
        
        login_response = self.client.post("/api/v1/auth/Signin", json={
            "username": self.username,
            "password" : self.password
        })

        if login_response.status_code not in [200 , 204]:
            print(f"Login failed for {self.username} with status {login_response.status_code}")
            return
        
        self.access_token = self.client.cookies.get("access_token")

        if not self.access_token:
            print(f"Failed to find access_token cookie for {self.username}")
            return
        
        base_ws_url = self.host.replace("http://", "ws://").replace("https://", "wss://")
        ws_endpoint = f"{base_ws_url}/ws/chat"

        try:
            # We explicitly pass the token inside the standard "Cookie" header frame
            # so Spring Security's handshake interceptor can read and validate it.
            self.ws = websocket.create_connection(
                ws_endpoint,
                header=[f"Cookie: access_token={self.access_token}"]
            )
            
            gevent.spawn(self._receive_loop)
            gevent.spawn(self._ping_loop)
            print(f"WebSocket successfully connected via Cookie for user: {self.username}")
            
        except Exception as e:
            events.request.fire(
                request_type="WebSocket",
                name="Connection Failure",
                response_time=0,
                response_length=0,
                exception=e
            )

    def _ping_loop(self):
        """
        Runs continuously in the background to send WebSocket pings at regular intervals.
        This helps to keep the connection alive and can also be used to measure basic network latency.
        """

        while self.ws and self.ws.connected:
            gevent.sleep(15)
            try:
                ping_payload = json.dumps({"type": "PING", "username": self.username})
                self.ws.send(ping_payload)
            except Exception:
                break

        

    def _receive_loop(self):
        """
        Runs continuously in the background for each active connection.
        Captures inbound packets to measure true P99 End-to-End message delivery latency.
        """
        while self.ws and self.ws.connected:
            try:
                message = self.ws.recv()
                data = json.loads(message)

                # Check if the incoming payload has our precision timestamp from the sender
                if "timestamp" in data:
                    # Calculate exactly how many milliseconds this message took to traverse
                    # through your Spring Boot App, Kafka brokers, and out to the consumer.
                    current_time_ms = int(time.time() * 1000)
                    e2e_latency = current_time_ms - int(data["timestamp"])

                    # Log this specific message receipt back to Locust's metric engine
                    events.request.fire(
                        request_type="WebSocket",
                        name="E2E Message Delivery",
                        response_time=e2e_latency,
                        response_length=len(message),
                        exception=None
                    )
            except websocket.WebSocketConnectionClosedException:
                break  # The connection was cleanly closed, kill the loop
            except Exception as e:
                events.request.fire(
                    request_type="WebSocket",
                    name="Receive Stream Error",
                    response_time=0,
                    response_length=0,
                    exception=e
                )
                break

    @task(4)
    def send_chat_message(self):
        """Simulates sending a high-volume chat message payload through Kafka partitions."""
        if self.ws and self.ws.connected:
            # Simulate a realistic variable chat room key to keep Kafka partitions balanced
            self.message_seq += 1
        
            # 1. CHOOSE AN EXISTING CONVERSATION
            # Instead of making new UUIDs, we pick one of the 5 pre-seeded chats at random.
            target_chat = random.choice(self.active_conversations)
            
            message_uuid = str(uuid.uuid4()) # Every individual message still gets a unique ID

            message_payload = {
            "id": message_uuid,
            "channel": target_chat["channel_id"],        # Reuses the exact same channel ID
            "message_seq": self.message_seq,
            "messageType": "CHAT",
            
            "from": {
                "userId": self.user_uuid,
                "username": self.username
            },
            "to": {
                "userId": target_chat["recipient_id"],   # Reuses the persistent friend ID
                "username": target_chat["recipient_name"]
            },
            
            "content": f"Hey! Chatting in our thread. Msg #{self.message_seq}",
            "ingressTimestampNanos": time.time_ns()
        }

        self.client.post(
            "/api/v1/channel/privateprivate/publishMessage",
            json=message_payload
        )

    @task(1)
    def send_typing_heartbeat(self):
        """Simulates a user typing. Tests high-frequency micro-payload handling."""
        if self.ws and self.ws.connected:
            payload = {
                "type": "TYPING",
                "senderId": self.user_id,
                "chatRoomId": f"room_{random.randint(1, 100)}",
                "status": "TYPING"
            }
            self._send_payload("Publish Typing Indicator", payload)

    def _send_payload(self, action_name, payload):
        """Helper matrix to safely push data over the socket pipe and report transmission speeds."""
        start_time = time.time()
        try:
            json_str = json.dumps(payload)
            self.ws.send(json_str)

            # Record successful transmission latency (how long it took to write to network buffer)
            events.request.fire(
                request_type="WebSocket",
                name=action_name,
                response_time=int((time.time() - start_time) * 1000),
                response_length=len(json_str),
                exception=None
            )
        except Exception as e:
            events.request.fire(
                request_type="WebSocket",
                name=action_name,
                response_time=int((time.time() - start_time) * 1000),
                response_length=0,
                exception=e
            )

    def on_stop(self):
        """Runs cleanly when a virtual user is torn down at the end of the test execution."""
        self.running = False
        if self.ws:
            self.ws.close()