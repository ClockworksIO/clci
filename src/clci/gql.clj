(ns clci.gql
  ""
  (:require
    [clojure.spec.alpha :as s]))


(s/def :gql.scalar.buildin/Int integer?)
(s/def :gql.scalar.buildin/Float float?)
(s/def :gql.scalar.buildin/String string?)
(s/def :gql.scalar.buildin/Boolean boolean?)
(s/def :gql.scalar.buildin/ID some?)


(def graphql-base-scalars
  ""
  [:gql.scalar.buildin/Int
   :gql.scalar.buildin/Float
   :gql.scalar.buildin/String
   :gql.scalar.buildin/Boolean
   :gql.scalar.buildin/ID])


(s/def :gh.gql.object/Releases
  (s/keys
    :req []
    :opt [:gh.repository/name
          :gh.repository/owner
          :gh.repository/releases]))


(s/def :gh.gql.object/ReleasesConnection
  (s/keys
    :req []
    :opt []))


(def gh-graphql-objects
  ""
  [:gh.gql.object/Repository
   :gh.gql.object/ReleasesConnection])


(def gh-graphql-schema
  "Schema describing the attributes of the Github Graphql API."
  [;; Github Object - Repository
   ;; https://docs.github.com/en/graphql/reference/queries#repository
   {:db.attribute/name          :gh.Repository/name
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/one
    :db.attribute/valueType     :gql.scalar.buildin/String
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "The name of the repository."}
   {:db.attribute/name          :gh.Repository/owner
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/one
    :db.attribute/valueType     :gql.scalar.buildin/String
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "The login field of a user or organization."}
   {:db.attribute/name          :gh.Repository/releases
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/one
    :db.attribute/valueType     :gh.gql.object.ReleasesConnection
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "List of releases which are dependent on this repository."}
   ;; Github Object - ReleaseConnection
   ;; https://docs.github.com/en/graphql/reference/objects#releaseconnection
   {:db.attribute/name          :gh.ReleasesConnection/edges
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/many
    :db.attribute/valueType     :gh.gql.object.ReleaseEdge
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "A list of edges."}
   {:db.attribute/name          :gh.ReleasesConnection/nodes
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/many
    :db.attribute/valueType     :gh.gql.object.Release
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "A list of nodes."}
   {:db.attribute/name          :gh.ReleasesConnection/pageInfo
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/many
    :db.attribute/valueType     :gh.gql.object.PageInfo
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "Information to aid in pagination."}
   {:db.attribute/name          :gh.ReleasesConnection/totalCount
    :db.attribute/unique        :db.unique/any
    :db.attribute/cardinality   :db.cardinality/one
    :db.attribute/valueType     :gql.scalar.buildin/Int
    :db.attribute/scope         :db.scope/global
    :db.attribute/doc           "Identifies the total count of items in the connection."}])
