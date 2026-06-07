import hashlib
import itertools
import json
import time
import random
import uuid
from locust import HttpUser, task, between, events
import websocket
import gevent

START_RANGE = 1
END_RANGE = 10000

# Thread-safe global counter matching your generate_series start point
user_id_generator = itertools.count(start=START_RANGE)

def get_deterministic_user_uuid(num: int)-> str: 
    hex_result = hashlib.md5(f"user_{num}".encode('utf-8')).hexdigest() 
    return str(uuid.UUID(hex_result))
    

class ChatUser(HttpUser):
    # Simulate realistic human pacing: wait 1 to 3 seconds between actions
    wait_time = between(1, 3)

    def on_start(self):
        """
        Runs automatically when a virtual user connects.
        Establishes a persistent WebSocket connection to your Spring Boot Monolith.
        """

        self.random_id = user_id_generator.__next__()

        if self.random_id > END_RANGE:
            print(f"Warning: Spawned user index {self.random_id} exceeds database bounds!")
            return
        
        self.user_uuid = get_deterministic_user_uuid(self.random_id)
        self.username = f"user_{self.random_id}"
        self.email = f"user_{self.random_id}@chat-loadtest.com"
        self.password = "$2a$10$7v5vFwYwY.."
        self.fullname = f"LoadTest User {self.random_id}"
        self.access_token: str | None = None
        self.refresh_token: str | None = None
        self.ws = None
        self.running = True
        self.fetched_channels = []

        
        login_response = self.client.post("/api/v1/auth/Signin", json={
            "username": self.username,
            "password" : self.password
        })

        if login_response.status_code not in [200 , 204]:
            print(f"Login failed for {self.username} with status {login_response.status_code}")
            return
        
        self.access_token = self.client.cookies.get_dict().get("access_token")

        if not self.access_token:
            print(f"Failed to find access_token cookie for {self.username}")
            return

        history_response = self.client.get("/api/v1/channel/private/?page=0&size=40")

        if history_response.status_code == 200:
            self.fetched_channels = history_response.json().get("list", [])
        
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
                if "ingressTimestampNanos" in data:
                    # Calculate exactly how many milliseconds this message took to traverse
                    # through your Spring Boot App, Kafka brokers, and out to the consumer.
                    current_time_ms = int(time.time() * 1000)
                    e2e_latency = current_time_ms - int(data["ingressTimestampNanos"])

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
    def chat_in_existing_channel(self):
        """Simulates sending a high-volume chat message payload through Kafka partitions."""
        if self.ws and self.ws.connected:
        
            # 1. CHOOSE AN EXISTING CONVERSATION
            target_chat = random.choice(self.fetched_channels)
            user1_dto = target_chat["user1"]
            user2_dto = target_chat["user2"]
            
            u1_id = user1_dto["id"]
            u2_id = user2_dto["id"]

            if self.user_uuid == u1_id:
                recipient_uuid = u2_id
                recipient_name = user2_dto["username"]
            else:
                recipient_uuid = u1_id
                recipient_name = user1_dto["username"]

            # message_uuid = str(uuid.uuid4()) # Every individual message still gets a unique ID

            message_payload = {
            "channel": target_chat["id"],        # Reuses the exact same channel ID
            "messageType": "TEXT",
            "from": {
                "userId": self.user_uuid,
                "username": self.username
            },
            "to": {
                "userId": recipient_uuid,   # Reuses the persistent friend ID
                "username": recipient_name
            },
            
            "content": f"Hey! Chatting in our thread, for Production-like load testing",
            # "ingressTimestampNanos": time.time_ns()
        }

        self.client.post(
            "/api/v1/channel/privateprivate/publishMessage",
            json=message_payload
        )
    

    @task(1)
    def chat_in_newchannel(self):
        """Simulates starting a new conversation and sending a message, testing channel creation and partition assignment."""
        if self.ws and self.ws.connected:
            # 1. CREATE A NEW CONVERSATION WITH A RANDOM RECIPIENT
            recipient_num = random.randint(START_RANGE, END_RANGE)
            recipient_uuid = get_deterministic_user_uuid(recipient_num)
            while recipient_uuid == self.user_uuid:
                recipient_num = random.randint(START_RANGE, END_RANGE)
                recipient_uuid = get_deterministic_user_uuid(recipient_num)
            
            recipient_name = f"user_{recipient_num}"

            # 2. CONSTRUCT THE MESSAGE PAYLOAD
            message_payload = {
                "messageType": "TEXT",
                "from": {
                    "userId": self.user_uuid,
                    "username": self.username
                },
                "to": {
                    "userId": recipient_uuid,
                    "username": recipient_name
                },
                "content": f"Hello {recipient_name}! This is a new conversation for load testing.",
                # "ingressTimestampNanos": time.time_ns()
            }

            # 3. SEND THE MESSAGE TO THE SERVER (WHICH SHOULD CREATE A NEW CHANNEL)
            self.client.post(
                "/api/v1/channel/private/publishMessage",
                json=message_payload
            )

    @task(1)
    def send_typing_heartbeat(self):
        """Simulates a user typing. Tests high-frequency micro-payload handling."""
        target_chat = random.choice(self.fetched_channels)
        user1_dto = target_chat["user1"]
        user2_dto = target_chat["user2"]
        
        u1_id = user1_dto["id"]
        u2_id = user2_dto["id"]

        if self.user_uuid == u1_id:
            recipient_uuid = u2_id
            recipient_name = user2_dto["username"]
        else:
            recipient_uuid = u1_id
            recipient_name = user1_dto["username"]
             
        if self.ws and self.ws.connected:
            payload = {
                "type": "TYPING",
                "from": self.user_id,
                "to": recipient_name,
                "channelId": target_chat["id"],
                "isTyping": True
            }
            self._send_payload("Publish Typing Indicator", payload)


    # def _send_payload(self, action_name, payload):
    #     """Helper matrix to safely push data over the socket pipe and report transmission speeds."""
    #     start_time = time.time()
    #     try:
    #         json_str = json.dumps(payload)
    #         self.ws.send(json_str)

    #         # Record successful transmission latency (how long it took to write to network buffer)
    #         events.request.fire(
    #             request_type="WebSocket",
    #             name=action_name,
    #             response_time=int((time.time() - start_time) * 1000),
    #             response_length=len(json_str),
    #             exception=None
    #         )
    #     except Exception as e:
    #         events.request.fire(
    #             request_type="WebSocket",
    #             name=action_name,
    #             response_time=int((time.time() - start_time) * 1000),
    #             response_length=0,
    #             exception=e
    #         )

    def on_stop(self):
        """Runs cleanly when a virtual user is torn down at the end of the test execution."""
        self.running = False
        if self.ws:
            self.ws.close()