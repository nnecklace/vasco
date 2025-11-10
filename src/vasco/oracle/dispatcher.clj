(ns vasco.oracle.dispatcher)

(defprotocol Dispatcher
  (reveal [this])
  (tell [this])
  (invoke [this system params]))
