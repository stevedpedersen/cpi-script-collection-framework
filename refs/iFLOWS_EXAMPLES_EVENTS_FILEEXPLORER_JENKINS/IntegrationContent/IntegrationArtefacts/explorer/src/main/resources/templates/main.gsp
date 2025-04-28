<!DOCTYPE html>
<html>
  <head>
    <title>$pageTitle</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">     
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
	  <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.7.0/css/all.css" integrity="sha384-lZN37f5QGtY3VHgisS14W3ExzMWZxybE1SJSEsQp9S+oqd12jhcu+A56Ebc1zFSJ" crossorigin="anonymous"> 
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.16.0/umd/popper.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>   
    <style>
		html {
			position: relative;
			min-height: 100%;
		}
		body {
		  /* Margin bottom by footer height */
        padding-top: 5em;
			  margin-bottom: 4em;
		}
		.footer {
			position: absolute;
			bottom: 0;
			width: 100%;
			height: 3.5em;
			line-height: 3.5em; /* Vertically center footer text */
		}		
      .card dl {
      	line-height: 1em;
      }
      .card dt {
        font-weight:normal;       
      }
      .fa-folder {
      	color: gold
      }
      .tr-narrow {
      	line-height: 1em;
      }
      @media only screen and (min-width: 576px) {
        .panel {
          width: 400px;
        }
      }         
    </style>
  </head>
  <body>
    <header>
      <nav class="navbar navbar-light bg-light navbar-expand-md fixed-top">
        <span class="navbar-brand">CPI Explorer</span>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button> 
        <div class="collapse navbar-collapse" id="navbarNav">        
	      <ul class="navbar-nav">
          <li class='nav-item <%if(action=='system') print 'active'%>'><a class='nav-link' href='system'>System</a></li>
          <li class='nav-item <%if(action=='bundles') print 'active'%>'><a class='nav-link' href='bundles'>Bundles</a></li>
          <li class='nav-item <%if(action=='classes') print 'active'%>'><a class='nav-link' href='classes'>Classes</a></li>
          <li class='nav-item <%if(action=='files') print 'active'%>'><a class='nav-link' href='files'>Files</a></li>
	      </ul>
        </div>
      </nav>
    </header>
		<main>
			<div class='container-fluid'>
<%=content%>
			</div>
		</main>
    <footer class="footer bg-light">
      <div class="container-fluid">
        <span class="text-muted ">CPI Explorer v.1.0.0 by <a href="https://indevo.pl">indevo</a> (2020)</span>
      </div>
    </footer>     
  </body>
</html>