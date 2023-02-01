(ns clci.gh
  "")


(defn gh-token
  ""
  []
  (System/getenv "GITHUB_TOKEN"))


(defn with-headers
  ""
  [m]
  (update m :headers assoc
          "Authorization" (str "token " (gh-token))))


(def some-query
  ""
  (fn [owner name]
    {'repository
     [:!args {:owner owner :name name}
      :description
      {:releases [:! {:last 100
                      :orderBy {:field :CREATED_AT :direction :DESC}}
                  {:nodes [:name
                           :createdAt
                           :url
                           {:releaseAssets [:! {:last 2}
                                            {:nodes [:name
                                                     :downloadCount
                                                     :downloadUrl]}]}]}]}]}))


;; "query {
;; 	rateLimit {
;; 		cost
;; 		remaining
;; 	}
;; 	repository(owner: "ClockworksIO", name: "clci") {
;; 		releases(last: 100, orderBy: { field: CREATED_AT, direction: DESC}) {
;; 			nodes {
;; 				name
;; 				createdAt
;; 				url
;; 				releaseAssets(last: 2) {
;; 					nodes {
;; 						name
;; 						downloadCount
;; 						downloadUrl
;; 					}
;; 				}
;; 			}
;; 		}
;; 	}
;; }"

(defn releases-query
  ""
  []
  {:query []
   :args {:$owner	"ClockworksIO"
          :$name	"clci"}})
