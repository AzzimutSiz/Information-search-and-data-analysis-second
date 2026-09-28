git clone https://github.com/AzzimutSiz/Information-search-and-data-analysis-second.git spark_project
cd spark_project
docker compose up --build -d

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8090/analyze" `
  -ContentType "application/json" `
  -Body '{"array":[1,2,3]}'
