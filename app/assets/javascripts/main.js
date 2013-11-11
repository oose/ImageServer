(function () {
    "use strict";

    // Angular must be first
    requirejs.config({
        priority: ["angular"]
    });

    // Define angular as a RequireJS module so it can be referenced in other defines
    define("angular", ["webjars!angular.js"], function () {
        return angular; // return the global var
    });


    require(["angular", "./controllers/controller"], function (a, ctrl) {
        var app = angular.module("app", []);
        app.controller("ServerCtrl", ctrl.ServerCtrl);

        angular.bootstrap(document, ["app"]);
    });
})();