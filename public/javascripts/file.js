'use strict';

var app = angular.module('fileUpload', ['ngFileUpload']);

app.controller(
  'MyCtrl',
  ['$rootScope', '$scope', 'Upload', '$timeout', '$http', function ($rootScope, $scope, Upload, $timeout, $http) {
    $scope.$watch('files', function() {
      $scope.upload($scope.files);
    });
    $scope.$watch('file', function() {
      if ($scope.file != null) {
        $scope.files = [$scope.file]; 
      }
    });
    $scope.log = '';
    $scope.allUploadFiles = [];

    $scope.upload = function(files) {
      if (files && files.length) {
        Array.prototype.push.apply($scope.allUploadFiles, files)

        for (var i = 0; i < files.length; i++) {
          var file = files[i];
          if (! file.$error) {
            file.upload = Upload.upload({
              url: $rootScope.routes.controllers.FileController.create().url,
              headers: {'Csrf-Token': 'nocheck'},
              data: {
                username: $scope.username,
                file: file  
              }
            })
            file.upload.then(function(resp) {
              $timeout(function() {
                file.stored = true;
                document.getElementById("storedFiles").contentDocument.location.reload(true);
                $scope.log = 'file: ' +
                  resp.config.data.file.name +
                  ', Response: ' + JSON.stringify(resp.data) +
                  '\n' + $scope.log;
              });
            }, function(resp) {
              // Error handler
              if (resp.status === 413) {
                file.$error = "TooBig";
              }
              else {
                file.$error = "Unknown";
              }
            }, function(evt) {
              var progressPercentage = parseInt(100.0 *
                    		                evt.loaded / evt.total);
              evt.config.data.file.progress = progressPercentage;
//            $scope.log = 'progress: ' + progressPercentage + 
//              '% ' + evt.config.data.file.name + '\n' + 
//              $scope.log;
            });
          }
        }
      }
    };
  }]
);
