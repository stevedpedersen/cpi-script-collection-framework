<div class='card panel mb-3'>
  <h6 class='card-header'>SAP CPI</h6>
  <div class='card-body'>
     <dl class='row'>
      <dt class='col-6'>Java version:</dt>
      <dd class='col-6'>${System.getProperty("java.version")}</dd>
      <dt class='col-6'>Groovy version:</dt>
      <dd class='col-6'>$GroovySystem.version</dd>
      <dt class='col-6'>Camel version:</dt>
      <dd class='col-6'>$camelVersion</dd>                      
    </dl>
  </div>
</div>      

<div class='card panel'>
  <h6 class='card-header'>Operating System</h6>
  <div class='card-body'>
    <dl class='row'>
      <dt class='col-6'>Name:</dt>
      <dd class='col-6'>${System.getProperty("os.name")}</dd>
      <dt class='col-6'>Version:</dt>
      <dd class='col-6'>${System.getProperty("os.version")}</dd>
      <dt class='col-6'>Architecture:</dt>
      <dd class='col-6'>${System.getProperty("os.arch")}</dd>
      <dt class='col-6'>Default charset:</dt>
      <dd class='col-6'>${java.nio.charset.Charset.defaultCharset()}</dd>
    </dl>
  </div>
</div>
