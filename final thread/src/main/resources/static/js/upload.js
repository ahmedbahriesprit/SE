document.getElementById('uploadForm').onsubmit = function(e) {
    e.preventDefault();
    var formData = new FormData(this);

    document.getElementById('progressBar').style.display = 'block';
    document.getElementById('progressText').innerText = 'Processing: 0%';
    document.getElementById('result').innerText = '';

    fetch('/upload', {
        method: 'POST',
        body: formData
    }).then(response => response.text())
        .then(result => {
            document.getElementById('result').innerHTML = result;
            clearInterval(progressInterval);
            document.getElementById('progressText').innerText = 'Processing: 100%';
            document.getElementById('progressBarFill').style.width = '100%';
        })
        .catch(error => {
            document.getElementById('result').innerHTML = 'Error: ' + error;
            clearInterval(progressInterval);
        });

    var progressInterval = setInterval(updateProgress, 1000);
};

function updateProgress() {
    fetch('/progress')
        .then(response => response.json())
        .then(progress => {
            var numThreads = document.getElementsByName('numThreads')[0].value;
            numThreads = numThreads > 0 ? numThreads : 1;  // Avoid division by zero
            var percent = (progress / numThreads) * 100;
            document.getElementById('progressBarFill').style.width = percent + '%';
            document.getElementById('progressText').innerText = 'Processing: ' + Math.round(percent) + '%';
        });
}