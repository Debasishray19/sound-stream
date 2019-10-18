tonguedemo = videoinput('winvideo', 1);
src = getselectedsource(tonguedemo);
tonguedemo.FramesPerTrigger = 1;
preview(tonguedemo);
start(tonguedemo);