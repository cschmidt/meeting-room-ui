(ns meeting-room-ui.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]))

;; FIXME: don't hard-code the client id
(def client-id "308276111773-emqvl2qrgpd695iqd91u0ublbb9d9ovc.apps.googleusercontent.com")


(def mount-target
  [:div#app
      [:h3 "Loading..."]])

(def home-page
  (html
   [:html {:itemscope ""
           :itemtype "http://schema.org/Article"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:meta {:name "google-signin-scope" :content "profile email"}]     [:meta {:name "google-signin-client_id" :content "308276111773-emqvl2qrgpd695iqd91u0ublbb9d9ovc.apps.googleusercontent.com"}]
     [:link {:rel "stylesheet" :type "text/css" :href "//cloud.typography.com/7928512/722606/css/fonts.css"}]
     [:script {:src "https://apis.google.com/js/platform.js" :async true :defer true}]
     [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"}]
     [:script {:src "https://apis.google.com/js/client:platform.js?onload=start"}]
     (include-css (if (env :dev) "css/style.css" "css/style.min.css"))]
    [:body
     mount-target
     (include-js "js/app.js")]]))


(defroutes routes
  (GET "/" [] home-page)

  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
