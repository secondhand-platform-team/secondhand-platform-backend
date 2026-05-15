import requests

url = "http://localhost:8000/order/api/orders/admin/statistics"
# Admin token might be needed if security is on, but I'll try without first or use a known token if I can find one.
# Wait, I can just call the service directly if it's exposed on 8083.
url = "http://localhost:8083/api/orders/admin/statistics"

try:
    response = requests.get(url)
    print(response.json())
except Exception as e:
    print(f"Error: {e}")
