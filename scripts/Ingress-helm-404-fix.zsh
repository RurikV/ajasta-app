 kubectl -n ajasta patch ingress ajasta-ingress --type json -p='[{"op":"remove","path":"/spec/rules/0/host"}]'
