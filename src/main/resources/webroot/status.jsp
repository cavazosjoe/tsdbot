<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
    <head>
        <meta http-equiv="Content-Type" CONTENT="text/html;charset=utf-8">
        <title>TSDIRC Status</title>
    </head>
    <body>
        <h1 style="text-align:center">Everything is terrible</h1>
        <p style="font-family: 'Courier New', Courier, monospace">
            <c:forEach items="${stats}" var="stat">
                ${stat.key}: ${stat.value}<br/>
            </c:forEach>
        </p>
    </body>
</html>