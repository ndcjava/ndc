<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
    <form action="http://47.99.57.122:8388/paygate/main" method="post">
        <input type="hidden" name="TransId" value="IPER"/>
    　　  <input type="hidden" name="Plain" value='<%=request.getAttribute("Plain")%>'>
    　　  <input type="hidden" name="Signature" value='<%=request.getAttribute("Signature")%>'>
    　　  <input type="submit" name="submit" value="银行支付网关">
     <input type="hidden" name="MerSeqNo" value="45454561454">
    </form>
</body>
</html>