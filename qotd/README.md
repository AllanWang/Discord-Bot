# QOTD

> Question/Quote of the Day

QOTD allows for automated posts + optional pings at a set time interval. For instance, you can have the bot submit a formatted question every day at 10am.
All information can be seen via `!qotd help`, but a typical flow to set up:

1. `!qotd init #channel` - only admins can do this, and it sets the configuration channel for QOTD. The next steps can only be done here.
1. `!qotd channel #post_channel` - specify which channel to post questions to
1. `!qotd time 10am` - set when to start posting. This relies on having `!timezone` configured (see [time module](/time)).
1. `!qotd timeInterval 24h` - set post frequency. This defaults to 24h, and at this stage, the bot will start posting.

The bot will post questions that you provide; you can add, list, and delete questions via `addQuestion`, `questions`, and `deleteQuestion`.
You can also configure templates and images that the bot will use for each post to format the responses.
