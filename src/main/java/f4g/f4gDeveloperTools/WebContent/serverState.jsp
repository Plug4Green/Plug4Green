<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<%@ page import="javax.xml.bind.JAXBContext" %> 
<%@ page import="javax.xml.bind.JAXBElement" %> 
<%@ page import="javax.xml.bind.JAXBException" %> 
<%@ page import="org.apache.commons.jxpath.JXPathContext" %> 
<%@ page import="org.f4g.core.IMain" %> 
<%@ page import="org.f4g.core.Main" %> 
<%@ page import="org.f4g.schema.metamodel.ServerType" %> 
<%@ page import="org.f4g.schema.actions.ActionRequestType" %> 
<%@ page import="org.f4g.schema.actions.PowerOffActionType" %> 
<%@ page import="org.f4g.schema.actions.PowerOnActionType" %> 
<%@ page import="java.util.List" %> 
<%@ page import="java.util.ArrayList" %> 

<%
	List<ServerType> servers = (List<ServerType>)session.getAttribute("SERVER_LIST");			

%>   
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Change server status</title>
</head>
<body>

<%for(ServerType server : servers){ %>
<%
	String action = "";
	if(server.getStatus().value().equals("ON")){
		action = "OFF";
	} else {
		action = "ON";
	}
	String href = "HandleServerAction?frameworkId="+server.getFrameworkID()+"&serverAction="+action;
%>
The server <%=server.getFrameworkID() %> is <%=server.getStatus().value() %> <a href="<%=href%>">Switch <%=action %> <%=server.getFrameworkID() %></a><br>
<%} %>

<br><br>
<% String href = "HandleServerAction?frameworkId=ALL&serverAction=ON"; %>
<a href="<%=href%>">Switch ON all servers which are off</a>

<br><br>
<% href = "HandleServerAction?frameworkId=ALL&serverAction=OFF"; %>
<a href="<%=href%>">Switch OFF all servers which are on</a>

</body>
</html>