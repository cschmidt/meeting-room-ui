(ns meeting-room-ui.core
    (:require [reagent.core :as reagent :refer [atom adapt-react-class]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.moment]
              [ajax.core :refer [GET POST]]
              [clojure.string :as s])
    (:import goog.History))

;; -------------------------
;; Data

(defonce room-schedule (reagent/atom {}))
(swap! room-schedule assoc :name "4th Floor - Meteor - HQ")
(swap! room-schedule assoc :current-booking {
  :summary "(meeting name)",
  :organizer {:name "(organizer)"}
  :starts-at "2015-10-27T09:00:00.000-07:00"
  :ends-at "2015-10-27T09:50:00.000-07:00"})
(swap! room-schedule assoc :next-booking nil)

(defn short-room-name [full-name]
  (-> (s/split full-name #"-") second s/trim))

(defn handle-room-update [data]
  (.log js/console (str "I got:" data))
  (reset! room-schedule data))

(defn update-schedule []
  (GET "http://something.unbounce.com:8080/calendar-summary"
       {:handler handle-room-update
        :keywords? true
        :response-format :json
        :with-credentials true}))

;; -------------------------
;; Components

(defn nav-bar []
  [:nav
    [:a {:href "#/"} "Home"]
    [:a {:href "#/about"} "About"]
    [:a {:href "#/meeting-room"} "Meeting Room"]]
    [:button {:name "Update" :on-click update-schedule} "Update"])

(defn home-page []
  [:div [:h2 "Welcome to the meeting room"]
  [:div {:class "g-signin2", :data-onsuccess "onSignIn", :data-theme "dark"}]
   [nav-bar]])

(defn format-time [datestring]
  (.format (js/moment. datestring) "h:mm a"))

(defn booking-component [booking]
  (let [{:keys [summary starts-at ends-at organizer]} booking]
    [:div.meeting
      [:div.meeting-time (format-time starts-at) " - " (format-time ends-at)]
      [:div.meeting-title summary]
      [:div.meeting-organizer [:span "Organized by: "] (get organizer :name "(unknown)")]
      [:div.meeting-participants "tbd"]]))

(defn footer-component [booking]
  (let [{:keys [summary starts-at ends-at organizer]} booking]
    [:div.meeting-footer
     [:div.label "Next Meeting:"]
     [:div.notification
      [:div.meeting-time (format-time starts-at) " - " (format-time ends-at)]
      [:div.meeting-title summary]]]))

(defn meeting-room-component [room-schedule]
  (let [{:keys [current-booking next-booking name]} room-schedule]
    [:div.meeting-container {:class (str "busy" " " (s/lower-case (short-room-name name)))}
      [:div.meeting-room-name (short-room-name name)]
      [:div.time (.format (js/moment.) "h:mm a")]
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
