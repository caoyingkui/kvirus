let log_editor;
let diff_editor;
let stompClient;

function initailaize(){
    log_editor_initialize();
    diff_editor_initialize();
    connect();




}

function connect(){
    let socket = new SockJS('/gs-guide-websocket')
    stompClient = Stomp.over(socket)
    stompClient.connect({}, function(frame){
        stompClient.subscribe("/message/commit", update_commit);
        stompClient.subscribe("/message/diff_file", update_files);
        stompClient.subscribe("/message/diff", update_diff);
    });
}


function diff_editor_initialize(){
    diff_editor = new AceDiff({
        element: '.acediff',
        left: {
            content: 'your first file content here',
        },
        right: {
            content: 'your second file content here',
        },
    });
}

function update_commit(commits){
    $("commit").innerText ="";
    $.each(commits, function(index, commit){
        $("commit").append("<option>" + commit +"</option>")
    });
}

function update_files(files){
    $("diff_file").innerText = "";
    $.each(files, function(index, file){
        $("diff_file").append("option" + file + "<>");
    });

}


function update_diff(diff){
    let former = diff["former"];
    let latter = diff["latter"];

    diff_editor.left.content = former;
    diff_editor.right.content = latter;
}

