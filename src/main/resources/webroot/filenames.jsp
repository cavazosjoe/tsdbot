<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
    <head>
        <meta http-equiv="Content-Type" CONTENT="text/html;charset=utf-8">
        <title>Filename Database</title>
    </head>
    <body>
    <h1>Official filename database of ${name}</h1>
    <ul>
        <c:forEach items="${filenames}" var="fname" varStatus="fnameIdx">
            <li>
                <a href="${serverUrl}/filenames/${fname.encoded}">${fname.unencoded}</a>
            </li>
        </c:forEach>
    </ul>
    </body>
</html>