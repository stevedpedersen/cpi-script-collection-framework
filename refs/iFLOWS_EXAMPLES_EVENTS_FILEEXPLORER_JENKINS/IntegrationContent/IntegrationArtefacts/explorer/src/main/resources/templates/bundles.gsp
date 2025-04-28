<form class="form-inline">
  <label for="name" class="mr-sm-2">Bundle name:</label>
	<input type="text" class="form-control col-sm-3" id="name" placeholder="Enter bundle name (e.g. *camel*)" name="name"<%if(namePattern){%> value='$namePattern'<%}%>>
</form> 
<br>
<%if(bundles) {%>
<table class="table table-striped table-sm">
  <thead>
    <tr>
      <th>Name</th>
      <th>Version</th>
      <th>Description</th>     
    </tr>
  </thead>
  <tbody>
  	<%bundles.each{ b-> %>
  	<tr>
      <td><a href='$b.link'>${b.name}</a></td>
      <td>$b.version</td>
      <td>$b.description</td>
    </tr>
    <%}%>
  </tbody>
</table>
<%}%>