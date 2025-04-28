<form class="form-inline">
  <label for="name" class="mr-sm-2">Class name:</label>
	<input type="text" class="form-control col-sm-3" id="name" placeholder="Enter class name (e.g. *camel*)" name="name"<%if(namePattern){%> value='$namePattern'<%}%>>
</form>
<br>
<%if(classes) {%>
<table class="table table-striped table-sm">
  <thead>
    <tr>
      <th>Class</th>
      <th>Bundle</th>
      <th>Download</th>
    </tr>
  </thead>
  <tbody>
  	<%classes.each{ c-> %>
  	<tr>
      <td>$c.name</td>
      <td><a href='$c.bundleLink'>$c.bundle</a></td>
      <td><%if(c.jarLink) {%><a href='$c.jarLink'>Download JAR</a><%}%></td>
    </tr>
    <%}%>
  </tbody>
</table>
<%}%>