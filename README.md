LB Health Check

Usage:
deploy on the application server with your app.

Have your load balancer configured to call /health URI

http 200 response - health OK

If memory, disk space or load tests fail a http 503 response will be returned.
