<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
    <head>
        <meta http-equiv="Content-Type" CONTENT="text/html;charset=utf-8">
        <title>TSDTV Catalog</title>
    </head>
    <body>
        <c:choose>
            <c:when test="${not empty param.show}">
                <h1><c:out value="${param.show}"/></h1>
                <a href="/tsdtv/catalog">Back to catalog</a>
            </c:when>
            <c:otherwise>
                <h1>TSDTV Catalog</h1>
                <h3>I would like to thank God almighty for giving everyone so much, and me so little</h3>
            </c:otherwise>
        </c:choose>
        <br/>
        <div style="font-family: 'Courier New', Courier, monospace">
            <c:forEach items="${files}" var="file">
                <c:choose>
                    <c:when test="${file.directory}">
                        <a href="/tsdtv/catalog?show=${file.name}"><c:out value="${file.name}"/></a>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${file.name}"/>
                        <form action="play" method="POST">
                            <input type="hidden" name="fileName" value="${file.name}">
                            <input type="hidden" name="show" value="${param.show}">
                            <input type="submit" value="Play">
                        </form>
                    </c:otherwise>
                </c:choose>
                <br/>
            </c:forEach>
        </div>
    </body>
</html>