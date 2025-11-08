# API cURL Collection

PowerShell setup:

```powershell
http://localhost:8080 = 'http://localhost:8080'
$USER_ID = 1
$POST_ID = 1
$COMMENT_ID = 1
```

All JSON payloads use single quotes for PowerShell.

## Users

Create user

```powershell
curl -X POST "http://localhost:8080/api/users" -H 'Content-Type: application/json' -d '{ "username": "dave", "name": "Dave" }'
```

List users

```powershell
curl "http://localhost:8080/api/users"
```

Get user by id

```powershell
curl "http://localhost:8080/api/users/$USER_ID"
```

Update user

```powershell
curl -X PUT "http://localhost:8080/api/users/$USER_ID" -H 'Content-Type: application/json' -d '{ "username": "dave2", "name": "Dave 2" }'
```

Delete user

```powershell
curl -X DELETE "http://localhost:8080/api/users/$USER_ID"
```

## Posts

Create post

```powershell
curl -X POST "http://localhost:8080/api/posts" -H 'Content-Type: application/json' -d '{ "userId": 1, "text": "Hello!" }'
```

List posts

```powershell
curl "http://localhost:8080/api/posts"
```

Get post by id

```powershell
curl "http://localhost:8080/api/posts/$POST_ID"
```

List posts by user

```powershell
curl "http://localhost:8080/api/posts/user/$USER_ID"
```

Update post

```powershell
curl -X PUT "http://localhost:8080/api/posts/$POST_ID" -H 'Content-Type: application/json' -d '{ "text": "Updated text" }'
```

Delete post

```powershell
curl -X DELETE "http://localhost:8080/api/posts/$POST_ID"
```

Feed (followed authors)

```powershell
curl "http://localhost:8080/api/posts/feed/$USER_ID?includeSelf=false"
curl "http://localhost:8080/api/posts/feed/$USER_ID?includeSelf=true"
```

## Comments

Create comment

```powershell
curl -X POST "http://localhost:8080/api/comments" -H 'Content-Type: application/json' -d '{ "postId": 1, "userId": 2, "text": "Nice post!" }'
```

Get comment by id

```powershell
curl "http://localhost:8080/api/comments/$COMMENT_ID"
```

List comments by post

```powershell
curl "http://localhost:8080/api/comments/post/$POST_ID"
```

Update comment

```powershell
curl -X PUT "http://localhost:8080/api/comments/$COMMENT_ID" -H 'Content-Type: application/json' -d '{ "text": "Edited comment" }'
```

Delete comment

```powershell
curl -X DELETE "http://localhost:8080/api/comments/$COMMENT_ID"
```

## Likes

Like a post

```powershell
curl -X POST "http://localhost:8080/api/likes" -H 'Content-Type: application/json' -d '{ "userId": 2, "postId": 1 }'
```

Unlike a post

```powershell
curl -X DELETE "http://localhost:8080/api/likes" -H 'Content-Type: application/json' -d '{ "userId": 2, "postId": 1 }'
```

Get likes for a post

```powershell
curl "http://localhost:8080/api/likes/post/$POST_ID"
```

## Follows

Follow user

```powershell
curl -X POST "http://localhost:8080/api/follows" -H 'Content-Type: application/json' -d '{ "followerId": 1, "followeeId": 2 }'
```

Unfollow user

```powershell
curl -X DELETE "http://localhost:8080/api/follows" -H 'Content-Type: application/json' -d '{ "followerId": 1, "followeeId": 2 }'
```

Get followees for user

```powershell
curl "http://localhost:8080/api/follows/$USER_ID"
```

List all follows

```powershell
curl "http://localhost:8080/api/follows"
```

## Hello (utility)

Hello

```powershell
curl "http://localhost:8080/hello"
```

Ping

```powershell
curl "http://localhost:8080/ping"
```

Sum

```powershell
curl "http://localhost:8080/sum?a=2&b=3"
```
