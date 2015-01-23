<html>
<head>
    <title>TSDTV</title>
    <script src="http://code.jquery.com/jquery-latest.js">
    </script>
    <script>
        var schedule_call = function() {
            $.get('/tsdtv/np',{},function(responseText) {
                $('#schedule').html(responseText);
            });
        };

        var interval = 1000 * 10; // every 20 seconds

        setInterval(schedule_call, interval);
    </script>
    <
</head>
<body style="background-color: midnightblue; color: white">
    <div style="width: 75%; float: left;">
        <embed autoplay="yes" target="http://irc.teamschoolyd.org:8090/tsdtv.flv"
               loop="true" name="VLC" type="application/x-vlc-plugin" volume="100" height="750" width="100%" id="vlc">
    </div>
    <div style="width: 20%; float: right;">
        <h2>Schedule</h2>
        <div id="schedule" style="font-family: 'Courier New', Courier, monospace"></div>
    </div>
</body>
</html>