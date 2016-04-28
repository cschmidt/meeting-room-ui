(ns meeting-room-ui.core
    (:require [reagent.core :as reagent :refer [atom adapt-react-class]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.moment]
              [cljsjs.bankersbox]
              [ajax.core :refer [GET POST]]
              [clojure.string :as s])
    (:import goog.History))

;; FIXME: don't hard-code client id
(def client-id "308276111773-emqvl2qrgpd695iqd91u0ublbb9d9ovc.apps.googleusercontent.com")

;; -------------------------
;; Data
(defonce clock (reagent/atom {}))


;; -------------------------
;; Clock
(defonce ds (js/BankersBox. 1))
(defn update-clock []
  (swap! clock assoc :current-time (.format (js/moment.) "h:mm a")))
(update-clock)
(js/setInterval update-clock 1000)


;; -------------------------
;; Authentication
(defonce login-state (reagent/atom {:logged-in? false}))

(defn handle-signed-in [response]
  (.log js/console (str "handle-signed-in" response))
  (aset js/window "auth_result" response)
  (swap! login-state assoc :logged-in? true))

(defn google-signin-callback [auth-result]
  (do
    (.log js/console "Signed in mostly...")
    (POST "http://localhost:8080/authorize"
          {:handler handle-signed-in
           :with-credentials true
           :format :json
           :params {:code (aget auth-result "code")} })
    (.log js/console (str "authorize response is" (aget auth-result "code")))))
  

(def signin-options (clj->js {"redirect_uri" "postmessage" 
                             "prompt" "consent select_account" 
                             "scope" "https://www.googleapis.com/auth/calendar.readonly https://www.googleapis.com/auth/userinfo.email"}))

(defn signin-clicked []
  (->
   (.grantOfflineAccess js/auth2 signin-options)
   (.then google-signin-callback)))

(defn logout-clicked []
  ;; FIXME: clear cookies? clear server-side state?
  (.log js/console "logout-clicked")
  (swap! login-state assoc :logged-in? false))

(defn init-google-auth []
  (.load 
   js/gapi 
   "auth2" 
   #(aset js/window "auth2" (.init js/gapi.auth2 {:client_id client-id}))))


(defonce room-schedule (reagent/atom {}))
;; FIXME: hard-coded room name
(swap! room-schedule assoc :name "4th Floor - Meteor - HQ")
(swap! room-schedule assoc :current-booking {
  :summary "(meeting name)",
  :organizer {:name "(organizer)"}
  :starts-at "2015-10-27T09:00:00.000-07:00"
  :ends-at "2015-10-27T09:50:00.000-07:00"})
(swap! room-schedule assoc :next-booking nil)

;; FIXME: allow for different room name formats, perhaps do this server-side
;; (feels like this should be provided in the API response, not computed
;; client-side)
(defn short-room-name [full-name]
  (-> (s/split full-name #"-") second s/trim))

(defn handle-room-update [data]
  (reset! room-schedule data))

(defn update-schedule []
;; FIXME: hard-coded URL
  (GET (str "http://localhost:8080/calendar-summary/"
            (.get ds "calendar-id"))
       {:handler handle-room-update
        :keywords? true
        :response-format :json
        :with-credentials true}))


(defn start-updates []
  (js/setInterval update-schedule 1000))

;; -------------------------
;; Components

(defn nav-bar []
  [:nav
   [:a {:href "#/"} "Home"]
   [:a {:href "#/setup"} "Setup"]
   [:a {:href "#/meeting-room"} "Meeting Room"]
   [:a {:href "#/logout"} "Logout"]])


(defn signin-component [login-state]
   [:button {:id "signInButton" :onClick signin-clicked} "Sign in with Google"])

(defn format-time [datestring]
  (.format (js/moment. datestring) "h:mm a"))

(defn booking-component [booking]
  (let [{:keys [summary starts-at ends-at organizer]} booking]
    [:div.meeting
      [:div.wrapper
        [:div.meeting-time (format-time starts-at) " - " (format-time ends-at)]
        [:div.meeting-title summary]
        [:div.meeting-organizer [:span "Organized by: "] (get organizer :email "(unknown)")]
        [:div.meeting-participants "tbd"]]]))

(defn footer-component [booking]
  (let [{:keys [summary starts-at ends-at organizer]} booking]
    [:div.meeting-footer
     [:div.label "Next Meeting:"]
     [:div.notification {:class "busy"}
      [:div.meeting-info
        [:div.meeting-time (format-time starts-at) " - " (format-time ends-at)]
        [:div.meeting-title summary]]]]))

(defn meeting-room-component [room-schedule]
  (let [{:keys [current-booking next-booking name current-time]} room-schedule
        {:keys [current-time]} @clock]
    [:div.meeting-container {:class (str (if current-booking "busy" "available") " " (s/lower-case (short-room-name name)))}
     [:div.meeting-header
      [:div.meeting-room-name (short-room-name name)]
      [:div.time {:id "clock"} current-time]]
    (if current-booking [booking-component current-booking])
    (if next-booking [footer-component next-booking])]))

;; -------------------------
;; Views

(defn home-page []
  (let [{:keys [logged-in?]} @login-state] 
    [:div 
     [:h2 "Welcome to Mr. D"]
     [:div "Resolving meeting room conflicts since 2016..."]
     (if logged-in?
       [nav-bar]
       [signin-component @login-state])]))

(defn meeting-room-page []
  [meeting-room-component @room-schedule])

(defn calendar-id-input [calendar-id]
  [:input {:type "text"
           :value @calendar-id
           :size 70
           :on-change #(do (reset! calendar-id (-> % .-target .-value))
                           (.set ds "calendar-id" @calendar-id)) }]
)

(defn setup-page []
  (let [calendar-id (reagent/atom (.get ds "calendar-id"))]
    [:div [:h2 "Setup meeting-room-ui"]
     "Specify the full ID of the Google calendar you want to display."
     [:div 
      [:label "Calendar ID"
       [calendar-id-input calendar-id]]]
     [:div [:a {:href "#/"} "go to the home page"]]]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(defn set-current-page [current-page]
  (session/put! :current-page current-page))

 

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/setup" []
  (session/put! :current-page #'setup-page))

(secretary/defroute "/meeting-room" []
  (session/put! :current-page #'meeting-room-page))

(secretary/defroute "/logout" []
  (logout-clicked)
  (session/put! :current-page #'home-page))

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
  (mount-root)
  (init-google-auth))

