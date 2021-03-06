(ns logatom.logger
  (:require [reagent.core    :as r]
            [cljs.spec        :as s]
            [datascript.core :as d]
            [posh.core       :as posh  :refer [pull posh! q transact!]]
            [cljs.pprint     :refer [pprint]]
            [alandipert.storage-atom :refer [local-storage]]
            [cljs.reader]
            [com.rpl.specter  :refer [ALL STAY MAP-VALS LAST
                                      stay-then-continue 
                                      collect-one comp-paths] :as sp]
            [clojure.string  :as str])
  (:require-macros
           [com.rpl.specter.macros  :refer [select transform declarepath providepath]]
           [reagent.ratom :refer [reaction]]))



(enable-console-print!)

(def schema {:todo/tags    {:db/cardinality :db.cardinality/many}
             :todo/project {:db/valuetype :db.type/ref}
             :todo/done    {:db/index true}
             :todo/due     {:db/index true}})


(def conn (r/atom (doto
                      (d/create-conn schema)
                    posh!)))


(defn index [xs]
  (map vector xs (range)))

(defn e-by-av [db a v]
  (-> (d/datoms db :avet a v) first :e))


(defn all-ents [db]
  (-> (d/pull-many db '[*]
        (select [ALL ALL]
                (d/q '[:find ?e :in $ :where [?e]] db)))
      pprint))



(def fixtures [
  [:db/add 0 :system/group :all]
  {:db/id -1
   :project/name "datascript"}
  {:db/id -2
   :project/name "nyc-webinar"}
  {:db/id -3
   :project/name "shopping"}
               
  {:todo/text "displaying list of todos"
   :todo/tags ["listen" "query"]
   :todo/project -2
   :todo/done true
   :todo/due  #inst "2014-12-13"}
  {:todo/text "persisting to localstorage"
   :todo/tags ["listen" "serialization" "transact"]
   :todo/project -2
   :todo/done true
   :todo/due  #inst "2014-12-13"}
  {:todo/text "make task completable"
   :todo/tags ["transact" "funs"]
   :todo/project -2
   :todo/done false
   :todo/due  #inst "2014-12-13"}
  {:todo/text "fix fn calls on emtpy rels"
   :todo/tags ["bug" "funs" "query"]
   :todo/project -1
   :todo/done false
   :todo/due  #inst "2015-01-01"}
  {:todo/text "add db filtering"
   :todo/project -1
   :todo/done false
   :todo/due  #inst "2015-05-30"}
  {:todo/text "soap"
   :todo/project -3
   :todo/done false
   :todo/due  #inst "2015-05-01"}
  {:todo/text "cake"
   :todo/done false
   :todo/project -3}
  {:todo/text "just a task" :todo/done false}
  {:todo/text "another incomplete task" :todo/done false}])






(def logatom (local-storage (atom {}) :logatom))



(defn transact-log [logatom conn ents]
  (let [txs (d/transact! @conn ents)
        datoms (:tx-data txs)
        txid   (nth (first datoms) 3)]
    (swap! logatom assoc txid {:datoms datoms})))




(def t!
  (partial transact-log logatom))


(def d2 (d/conn-from-datoms (select [MAP-VALS :datoms ALL] @logatom) schema))


(reset! conn (doto d2 posh!))


@@conn



(add-watch logatom
           :logatom
           (fn [_ _ _ v]
             (.log js/console "Logging" v)))




(->> (d/transact! @conn [{:todo/text "just a tkljklask" :todo/done false}])
    )  
@@conn



(t! conn fixtures)



(def d2 (d/with (d/empty-db) 
         (mapv #(concat [(if (nth % 4) :db/add :db/retract)] %)
               (first (select [MAP-VALS :datoms] @logatom)))))




(all-ents @d2)
