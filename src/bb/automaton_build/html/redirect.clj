(ns automaton-build.html.redirect "Creates an html page to redirect to another page.")

(defn page
  "Redirect page in the `url`."
  [url]
  (format
   "<!DOCTYPE html>
<html>
<body>

<h2>Redirect to a Webpage</h2>
<p>Click <a href=\"%s\">here</a> if the redirection does not happen automatically.</p>

<button onclick=\"myFunction()\">Replace document</button>

<script>
function myFunction() {
  location.replace(\"%s\")
}
(myFunction)
</script>
</body>
</html>"
   url
   url))
