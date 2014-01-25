# awizo

A clojure library for attaching functions to filesystem events.

## Leiningen

```
[awizo "0.0.1"]
```

## Usage

```clojure

(defn context [e]
 (prn (.context e)))
 
(awizo/attach-handler "/path/to/folder" context [awizo/MODIFY])
```
                      
## License

Copyright © 2014 Aaron France

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
