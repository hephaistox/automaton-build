(ns automaton-build.utils.time
  "Adapter to time functions.

  Like formatting the date from #inst to str and way back
  Will be a proxy for `clojure.instant`, `java.text.SimpleDateFormt`")

(defn now-str
  "Return the time we are now"
  []
  (.format (java.text.SimpleDateFormat. "EEE MMM d HH:mm:ss zzz yyyy") (java.util.Date.)))
