# github-rank

  Program returns the list of organization contributors sorted by the number of contributions.
   It can be executed by following instructions:
   
   - Sbt run(from root directory)
    
    sigurd@sigurd:~/projects/github-rank> sbt run 
    
  Launched server will listen localhost on 8080 port. You can use curl to make requests e.g.
  
    sigurd@sigurd:~/projects/github-rank> curl localhost:8080/org/sigurdthor/contributors
    
  Note: to handle Github API rate limit restriction set environment variable GH_TOKEN as following:
  
    sigurd@sigurd:~/projects/github-rank> GH_TOKEN=2c30c4fa74eba4f181245bbb5ab0acacac18eccd; export GH_TOKEN  
    