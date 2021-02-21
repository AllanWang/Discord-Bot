# Game Event

> Schedule events with attendance and queues

Game Events can keep track of player counts, and provide dynamic times for the event.
To use it, call `!gameEvent create [message]` to create a new event. 
The bot will send the message verbatim, and provide lists for `yes`, `maybe`, and `no`.
Others can then react and be added to a list.

If a time is added in the message, the bot will parse it. If the event creator has set their timezone via `!timezone`, the time will be added on the bottom right.
The bottom right time is dynamic, so everyone will see it in their respective timezones.