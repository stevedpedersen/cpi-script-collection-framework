<form class="form-inline">
  <label for="name" class="mr-sm-2">Directory:</label>
	<input type="text" class="form-control col-sm-3" id="name" placeholder="Enter directory (e.g. /home)" name="dir"<%if(dir){%> value='$dir.path'<%}%>>
</form>
<br>
<table class="table table-borderless table-sm">
  <thead>
    <tr>
      <th>Name</th>
      <th>Modification Date</th>
      <th>Size</th>
    </tr>
  </thead>
  <tbody>
  <%if(dir.parent) {%>
  	<tr class='tr-narrow'>
      <td><a href='${script.directoryLink(dir.parent)}' class='text-body'><i class='fas fa-folder'></i> ..</a></td>
    </tr>  
  <%}%>
  <%dir.eachDir { f -> %>
  	<tr class='tr-narrow'>
      <td><a href='${script.directoryLink(f.absolutePath)}' class='text-body'><i class='fas fa-folder'></i> $f.name</a></td>
      <td>${dateFormat.format(new Date(f.lastModified()))}</td>
    </tr>  
  <%}%>
  <%dir.eachFile(groovy.io.FileType.FILES) { f-> %>
  	<tr class='tr-narrow'>
      <td><a href='${script.downloadLink(f.absolutePath)}' class='text-body'><i class='far fa-file'></i> $f.name</a></td>
      <td>${dateFormat.format(new Date(f.lastModified()))}</td>
      <td>${f.size().intdiv(1024)} KB</td>
    </tr>  
  <%}%>
  
  </tbody>
</table>