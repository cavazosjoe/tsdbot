<!DOCTYPE html>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>TSDTV 2.0 rev.D</title>

    <%--favicon--%>
    <link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
    <link rel="icon" href="/favicon.ico" type="image/x-icon">

    <!-- Bootstrap -->
    <link href="css/darkly/bootstrap.min.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <script src="js/jquery.bootstrap-growl.min.js"></script>

    <%--video.js--%>
    <link href="//vjs.zencdn.net/4.11/video-js.css" rel="stylesheet">
    <script src="//vjs.zencdn.net/4.11/video.js"></script>

    <script>

        (function update_nowplaying() {
            $.get('/tsdtv/np?type=nowplaying',{},function(responseText) {
                $('#nowplaying').html(responseText).text();
            }).then(function() {
                setTimeout(update_nowplaying, 1000 * 10);
            });
        })();

        (function update_queue() {
            $.get('/tsdtv/np?type=queue',{},function(responseText) {
                $('#queue').html(responseText).text();
            }).then(function() {
                setTimeout(update_queue, 1000 * 10);
            });
        })();

        (function update_blocks() {
            $.get('/tsdtv/np?type=blocks',{},function(responseText) {
                $('#blocks').html(responseText).text();
            }).then(function() {
                setTimeout(update_blocks, 1000 * 60 * 10);
            });
        })();

        (function update_viewers() {
            $.get('/tsdtv/np?type=viewers',{},function(responseText) {
                $('#viewers').html(responseText).text();
            }).then(function() {
                setTimeout(update_viewers, 1000 * 10);
            });
        })();

        $(function() {
            var form = $('.playEpisode');
            $(form).submit(function (event) {
                $.ajax({
                    type: "POST",
                    url: "/tsdtv/play", //process to mail
                    data: $(this).serialize(),
                    success: function (msg) {
                        $('#episodePlayModal').modal('show');
                    },
                    error: function(xhr, status, error) {
                        $.bootstrapGrowl(xhr.responseText, {
                            type: 'danger',
                            width: 'auto',
                            allow_dismiss: true
                        });
                    }
                });
                event.preventDefault();
            });

        });
    </script>
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <div class="col-md-9">
            <c:choose>
                <c:when test="${not empty param.vlc}">
                    <embed autoplay="yes" target="${directLink}"
                       loop="true" name="VLC"
                       type="application/x-vlc-plugin" volume="100" height="750" width="100%" id="vlc">
                    <a href="/tsdtv">videojs version</a>
                </c:when>
                <c:otherwise>
                    <video id="example_video_1" class="video-js vjs-default-skin"
                           controls loop preload="auto" width="100%" height="750px"
                           poster="http://i.imgur.com/4Q7jsCr.jpg"
                           data-setup='{"autoplay": true}'>
                        <source src="${directLink}" type='${videoFmt}' />
                        <p class="vjs-no-js">To view this video please enable JavaScript, and consider upgrading to a web browser that <a href="http://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a></p>
                    </video>
                    <a href="/tsdtv?vlc=yes">VLC version</a>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="col-md-3">

            <%--Collection of right-hand tabs--%>
            <div role="tabpanel">

                <!-- Nav tabs -->
                <ul class="nav nav-tabs nav-justified" role="tablist">
                    <li role="presentation" class="active"><a href="#schedule" aria-controls="schedule" role="tab" data-toggle="tab">Schedule</a></li>
                    <li role="presentation"><a href="#catalogTab" aria-controls="catalogTab" role="tab" data-toggle="tab">Catalog</a></li>
                    <li role="presentation"><a href="#chat" aria-controls="chat" role="tab" data-toggle="tab">Chat</a></li>
                </ul>

                <!-- Tab panes -->
                <div class="tab-content">

                    <%--Schedule--%>
                    <div role="tabpanel" class="tab-pane active" id="schedule">

                        <%--now playing--%>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h3 class="panel-title" style="color: #00bc7e"><strong>NOW PLAYING</strong></h3>
                            </div>
                            <div class="panel-body">
                                <div id="nowplaying">Loading...</div>
                            </div>
                        </div>

                        <%--next in the queue--%>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h3 class="panel-title" style="color: #00bc7e">Up next</h3>
                            </div>
                            <div class="panel-body">
                                <div id="queue">Loading...</div>
                            </div>
                        </div>

                        <%--blocks that will play later today--%>
                        <div id="blocks">Loading...</div>

                    </div>

                    <%--Catalog--%>
                    <div role="tabpanel" class="tab-pane" id="catalogTab" style="max-height: 700px; overflow-y: scroll;">
                        <div class="panel-group" id="catalog" role="tablist" aria-multiselectable="true">
                            <c:forEach items="${catalog}" var="show" varStatus="showIdx">
                                <%--Show--%>
                                <c:set var="expand" value="${showIdx.first}"/>
                                <div class="panel panel-default">
                                    <div class="panel-heading" role="tab" id="<c:out value="${show.key.rawName}"/>-head">
                                        <h4 class="panel-title">
                                            <a ${expand ? '' : 'class="collapsed"'} data-toggle="collapse" data-parent="#catalog" href="#<c:out value="${show.key.rawName}"/>-content" aria-expanded="${expand}" aria-controls="<c:out value="${show.key.rawName}"/>-content">
                                                <c:out value="${show.key.prettyName}"/>
                                            </a>
                                        </h4>
                                    </div>
                                    <div id="<c:out value="${show.key.rawName}"/>-content" class="panel-collapse collapse" role="tabpanel" aria-labelledby="<c:out value="${show.key.rawName}"/>-head">
                                        <div class="panel-body">
                                            <ul class="list-group">
                                                <c:forEach items="${show.value}" var="episode">
                                                    <%--Episode--%>
                                                    <li class="list-group-item">
                                                        <c:out value="${episode.prettyName}"/>
                                                        <form class="playEpisode" name="playEpisode">
                                                            <input type="hidden" name="fileName" value="${episode.rawName}">
                                                            <input type="hidden" name="show" value="${show.key.rawName}">
                                                            <input class="btn btn-sm btn-success" type="submit" value="Play" >
                                                        </form>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <%--Chat ;^)--%>
                    <div role="tabpanel" class="tab-pane" id="chat">;^)</div>

                </div>

            </div>
            <div>
                Viewers: <span id="viewers" class="badge">...</span>
            </div>
        </div>
    </div>
</div>
<div class="modal fade" id="episodePlayModal" tabindex="-1" role="dialog" aria-labelledby="episodePlayModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title" id="episodePlayModalLabel">You did it!</h4>
            </div>
            <div class="modal-body">
                Your show has been enqueued :^)
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
<div class="modal fade" id="controlModal" tabindex="-1" role="dialog" aria-labelledby="control-modal-label" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title" id="control-modal-label">Nothing...</h4>
            </div>
            <div id="control-modal-body" class="modal-body">
                Nothing...
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
</body>
</html>