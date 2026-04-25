WITH ordered_messages AS(
 SELECT message_id, ROW_NUMBER() OVER(
   PARTITION BY private_channel_id
   ORDER BY sent_at) AS seq
   FROM private_message
)

UPDATE private_message pm
SET message_seq = om.seq
FROM ordered_messages om
where pm.message_id = om.message_id;
