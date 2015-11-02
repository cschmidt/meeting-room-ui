(ns meeting-room-ui.prod
  (:require [meeting-room-ui.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
