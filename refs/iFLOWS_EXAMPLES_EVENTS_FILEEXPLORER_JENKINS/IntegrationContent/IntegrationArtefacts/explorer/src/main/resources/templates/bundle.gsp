<h3>Bundle $bundle.symbolicName <small>(ID: $bundle.bundleId)</small></h3>
<h4>Location: $bundle.location
<%if(jarLink) {%><br><a href='$jarLink'>Download</a><%}%>
</h4>
<%if(resources) {%>
<br>
<h5>Resources:</h5>
<table class="table table-borderless table-sm">
  <tbody>
  	<%resources.each{ r-> %>
  	<tr class='tr-narrow'>
      <td>$r</td>
    </tr>
    <%}%>
  </tbody>
</table>
<%}%>
<%if(entries) {%>
<br>
<h5>Entries:</h5>
<table class="table table-borderless table-sm">
  <tbody>
  	<%entries.each{ e-> %>
  	<tr class='tr-narrow'>
      <td>$e.path</td>
    </tr>
    <%}%>
  </tbody>
</table>
<%}%>