<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.TimeZone" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
    <head>
        <meta http-equiv="Content-Type" CONTENT="text/html;charset=utf-8">
        <title>TSDTV Play</title>
    </head>
    <body>
        <div style="font-family: 'Courier New', Courier, monospace">
            <c:choose>
                <c:when test="${param.result == 'true'}">
                    Thank you. Your show is playing RIGHT NOW NOW NOW <a href="http://irc.teamschoolyd.org/tsdtv.html" target="_blank">GO GO GO</a>
                </c:when>
                <c:when test="${param.result == 'false'}">
                    Thank you. Your show has been enqueued.
                    <br/>
                    <br/>
                    Scheduled:
                    <c:forEach items="${queue}" var="movie" varStatus="loopStatus">
                        <br/>
                        <c:choose>
                            <c:when test="${loopStatus.first}">
                                Now Playing
                            </c:when>
                            <c:otherwise>
                                <fmt:formatDate value="${movie.startTime}" pattern="HH:mm z" timeZone="America/New_York"/>
                            </c:otherwise>
                        </c:choose>
                        <c:out value=" - ${movie.toPrettyString()}"/>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    Something went wrong. What did you do?
                </c:otherwise>
            </c:choose>
        </div>
        <p>
            <a href="/tsdtv/catalog">Back to catalog</a>
        </p>
    </body>
</html>