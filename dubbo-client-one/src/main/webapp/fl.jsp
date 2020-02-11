<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
    <form id="form1" name="form1" action="https://epay1.zj96596.com/paygate/main" method="post">
        <input type="hidden" name="TransId" value="IPER"/>
    　　  <input type="hidden" name="Plain" value='<%=request.getAttribute("Plain")%>'>
    　　  <input type="hidden" name="Signature" value='<%=request.getAttribute("Signature")%>'>
    </form>
    
<script>
function load_submit()
{
document.form1.submit();
}
load_submit();
</script>
</body>
</html>