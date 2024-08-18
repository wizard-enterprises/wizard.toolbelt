(ns wizard.toolbelt
  (:require [wizard.toolbelt.utils :as utils]
            tupelo.core))

(utils/intern-all-from *ns* 'wizard.toolbelt.utils)
(utils/intern-all-from *ns* 'tupelo.core)
