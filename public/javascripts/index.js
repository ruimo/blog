var removeArticle = function(id, confirmMsg, yes, no) {
  remove(id, confirmMsg, yes, no, "#removeArticleForm");
}

var removeComment = function(id, confirmMsg, yes, no) {
  remove(id, confirmMsg, yes, no, "#removeCommentForm");
}

var remove = function(id, confirmMsg, yes, no, formName) {
  var dlg = $("#confirmDialog");
  dlg.text(confirmMsg);
  dlg.dialog({
    modal: true,
    buttons: [
      {
        text: yes,
        class: 'yes-button',
        click: function() {
          $(formName + " .id").attr("value", id);
          $(formName).submit();
        }
      },    
      {
        text: no,
        class: 'no-button',
        click: function() {
          $(this).dialog('close');
        }
      }
    ]
  });
};
