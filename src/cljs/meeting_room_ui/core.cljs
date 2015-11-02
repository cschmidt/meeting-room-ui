(ns meeting-room-ui.core
    (:require [reagent.core :as reagent :refer [atom adapt-react-class]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.moment])
    (:import goog.History))

;; -------------------------
;; Data

(def room-schedule-data
  {:name "Meteor"
   :current-booking {
     :summary "Amazon QuickSight: A Fast, Cloud-Powered BI Service Confirmation",
     :organizer "Webinars Aws"
     :starts-at "2015-10-27T09:00:00.000-07:00"
     :ends-at "2015-10-27T10:00:00.000-07:00"}
   :next-booking nil})

(defonce room-schedule (reagent/atom nil))

;; Components

;; -------------------------
;; Views

(defn nav-bar []
  [:nav
    [:a {:href "#/"} "Home"]
    [:a {:href "#/about"} "About"]
    [:a {:href "#/meeting-room"} "Meeting Room"]])

(defn home-page []
  [:div [:h2 "Welcome to meeting-room-ui"]
   [nav-bar]])

(defn format-time [datestring]
  (.format (js/moment. datestring) "h:mm a"))

(defn event-component [event]
 [:div.event
   [:h2.eventName (get event :summary)]
   [:div [:span (format-time (get event :starts-at))] " - "
         [:span (format-time (get event :ends-at))]]
   [:div "by " [:span (get event :organizer)]]])

(defn meeting-room-component [room]
  [:div.room {:class "room occupied"}
    [:div.clock
      [:span.time (.format (js/moment.) "h:mm a")]
      [:span.date (.format (js/moment.) "MMM Do")]]
    [:div.roomName [:h1 (get room :name)]]
    [event-component (get room :current-booking)]])

(defn meeting-room []
  [:div
    [meeting-room-component room-schedule-data]
    [nav-bar]])

(defn about-page []
  [:div [:h2 "About meeting-room-ui"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/meeting-room" []
  (session/put! :current-page #'meeting-room))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
