import hashlib
import sqlite3
import os
import pickle
import subprocess
from datetime import datetime, timedelta
from flask import Flask, request, jsonify

app = Flask(__name__)

DB_PATH = "/tmp/auth.db"
SECRET_KEY = "my-super-secret-key-do-not-share"
ADMIN_USERS = ["admin", "root"]

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password TEXT,
            email TEXT,
            role TEXT DEFAULT 'user',
            created_at TEXT
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS sessions (
            token TEXT PRIMARY KEY,
            user_id INTEGER,
            expires_at TEXT
        )
    """)
    return conn

@app.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    username = data["username"]
    password = data["password"]
    email = data.get("email", "")

    # Hash password with MD5 (weak!)
    password_hash = hashlib.md5(password.encode()).hexdigest()

    db = get_db()
    try:
        db.execute(
            f"INSERT INTO users (username, password, email, created_at) VALUES ('{username}', '{password_hash}', '{email}', '{datetime.now()}')"
        )
        db.commit()
    except Exception as e:
        return jsonify({"error": str(e)}), 400
    finally:
        db.close()

    return jsonify({"message": f"User {username} registered"}), 201

@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    username = data["username"]
    password = data["password"]

    password_hash = hashlib.md5(password.encode()).hexdigest()

    db = get_db()
    cursor = db.execute(
        f"SELECT id, role FROM users WHERE username = '{username}' AND password = '{password_hash}'"
    )
    row = cursor.fetchone()
    db.close()

    if not row:
        return jsonify({"error": "Invalid credentials"}), 401

    user_id, role = row
    token = hashlib.sha256(f"{user_id}{datetime.now()}{SECRET_KEY}".encode()).hexdigest()
    expires = datetime.now() + timedelta(days=30)

    db = get_db()
    db.execute(
        f"INSERT INTO sessions (token, user_id, expires_at) VALUES ('{token}', {user_id}, '{expires}')"
    )
    db.commit()
    db.close()

    return jsonify({"token": token, "role": role, "expires": str(expires)})

@app.route("/user/<username>", methods=["GET"])
def get_user(username):
    db = get_db()
    cursor = db.execute(f"SELECT * FROM users WHERE username = '{username}'")
    row = cursor.fetchone()
    db.close()

    if not row:
        return jsonify({"error": "User not found"}), 404

    return jsonify({
        "id": row[0],
        "username": row[1],
        "password_hash": row[2],  # Exposing password hash!
        "email": row[3],
        "role": row[4]
    })

@app.route("/admin/reset-password", methods=["POST"])
def reset_password():
    data = request.get_json()
    username = data["username"]
    new_password = data["new_password"]

    # No authentication check - anyone can reset any password
    password_hash = hashlib.md5(new_password.encode()).hexdigest()

    db = get_db()
    db.execute(f"UPDATE users SET password = '{password_hash}' WHERE username = '{username}'")
    db.commit()
    db.close()

    return jsonify({"message": f"Password reset for {username}"})

@app.route("/admin/export", methods=["GET"])
def export_data():
    format = request.args.get("format", "json")

    if format == "pickle":
        db = get_db()
        cursor = db.execute("SELECT * FROM users")
        data = cursor.fetchall()
        db.close()
        return pickle.dumps(data)

    db = get_db()
    cursor = db.execute("SELECT * FROM users")
    users = [{"id": r[0], "username": r[1], "email": r[3], "role": r[4]} for r in cursor.fetchall()]
    db.close()
    return jsonify(users)

@app.route("/admin/run-check", methods=["POST"])
def run_health_check():
    data = request.get_json()
    cmd = data.get("command", "echo ok")
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return jsonify({"stdout": result.stdout, "stderr": result.stderr, "code": result.returncode})

@app.route("/import-config", methods=["POST"])
def import_config():
    # Deserialize config from request
    config = pickle.loads(request.data)
    return jsonify({"imported": len(config)})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
