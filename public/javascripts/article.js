var submitArticle = function(form) {
  $("#submitButton").prop('disabled', true);
  var formData = new FormData(form);
  $.ajax({
    url: $(form).attr("action"),
    method: $(form).attr("method"),
    dataType: "json",
    data: formData,
    processData: false,
    contentType: false
  }).done(function(data, status, jqXhr) {
    $("#submitButton").prop('disabled', false);
    location.href='/';
  }).fail(function(jqXhr, textStatus, errorThrown) {
    $("#submitButton").prop('disabled', false);
    console.log('fail', textStatus);
    if (jqXhr.responseJSON.status === "ValidationError") {
      console.log('validation error', JSON.stringify(jqXhr.responseJSON));
      $("#articleForm").html(jqXhr.responseJSON.form);
    }
  });
};

$(function() {
  $('.dateTime').datetimepicker();
});

var app = angular.module('articlePreview', ['ngSanitize']);
app.controller("MainCtrl", function($scope) {
  $scope.articleHtml = $("#body").text();
});
