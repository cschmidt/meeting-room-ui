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

(defonce room-schedule (reagent/atom {}))
(swap! room-schedule assoc :name "Meteor")
(swap! room-schedule assoc :current-booking {
  :summary "Amazon QuickSight: A Fast, Cloud-Powered BI Service Confirmation",
  :organizer {:name "Webinars Aws"}
  :starts-at "2015-10-27T09:00:00.000-07:00"
  :ends-at "2015-10-27T09:50:00.000-07:00"})
(swap! room-schedule assoc :next-booking nil)

;; -------------------------
;; Components

(defn nav-bar []
  [:nav
    [:a {:href "#/"} "Home"]
    [:a {:href "#/about"} "About"]
    [:a {:href "#/meeting-room"} "Meeting Room"]])

(defn home-page []
  [:div [:h2 "Welcome to meeting-room-ui"]
  [:div {:class "g-signin2", :data-onsuccess "onSignIn", :data-theme "dark"}]
   [nav-bar]])

(defn format-time [datestring]
  (.format (js/moment. datestring) "h:mm a"))

(defn booking-component [booking]
  (let [{:keys [summary starts-at ends-at organizer]} booking]
    [:div.event
      [:h2.eventName summary]
      [:div [:span (format-time starts-at)] " - "
            [:span (format-time ends-at)]]
   [:div "by " [:span (get organizer :name)]]]))

(defn meeting-room-component [room-schedule]
  (let [{:keys [current-booking next-booking name]} room-schedule]
    [:div.room {:class "room occupied"}
      [:div.clock
        [:span.time (.format (js/moment.) "h:mm a")]
        [:span.date (.format (js/moment.) "MMM Do")]]
      [:div.roomName [:h1 name]]
    (if current-booking [booking-component current-booking])
    (if next-booking [booking-component next-booking])]))

;; -------------------------
;; Views

(defn meeting-room-page []
  [:div
    [meeting-room-component @room-schedule]
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
  (session/put! :current-page #'meeting-room-page))

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
