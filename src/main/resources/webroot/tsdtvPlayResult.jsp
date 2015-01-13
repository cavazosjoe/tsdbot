<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
    <head>
        <meta http-equiv="Content-Type" CONTENT="text/html;charset=utf-8">
        <title>TSDTV Play</title>
    </head>
    <body>
        <p style="font-family: 'Courier New', Courier, monospace">
            <c:choose>
                <c:when test="${param.result == 'true'}">
                    Thank you. Your show is playing RIGHT NOW NOW NOW GO GO GO
                </c:when>
                <c:when test="${param.result == 'false'}">
                    Thank you. Your show has been enqueued.
                </c:when>
                <c:otherwise>
                    Something went wrong. What did you do?
                </c:otherwise>
            </c:choose>
        </p>
        <p>
            <a href="/tsdtv/catalog">Back to catalog</a>
        </p>
    </body>
</html>