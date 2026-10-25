#!/usr/bin/env python3
"""Seed the five local test users; reads secrets only from ignored .env. No token output."""
import json
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
ROLES = ['ADMINISTRADOR', 'CAJERO', 'REPARTIDOR', 'ABASTECIMIENTO', 'CLIENTE']

def environment():
    return dict(line.split('=', 1) for line in (ROOT / '.env').read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#') and '=' in line)

def request(path, data=None, token=None, method=None, form=False):
    headers = {}
    if token: headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        data = (urlencode(data) if form else json.dumps(data)).encode()
    with urlopen(Request('http://localhost:38080' + path, data=data, headers=headers, method=method), timeout=30) as response:
        raw = response.read()
        return json.loads(raw) if raw else None

def main():
    env = environment()
    token = request('/realms/master/protocol/openid-connect/token', {
        'grant_type': 'password', 'client_id': 'admin-cli',
        'username': env['KEYCLOAK_ADMIN_USERNAME'], 'password': env['KEYCLOAK_ADMIN_PASSWORD'],
    }, form=True)['access_token']
    for role in ROLES:
        username = role.lower()
        users = request('/admin/realms/polleria/users?' + urlencode({'username': username, 'exact': 'true'}), token=token)
        if not users:
            request('/admin/realms/polleria/users', {'username': username, 'enabled': True,
                'firstName': role.capitalize(), 'lastName': 'Prueba', 'email': username + '@polleria.test', 'emailVerified': True,
                'requiredActions': []}, token)
            users = request('/admin/realms/polleria/users?' + urlencode({'username': username, 'exact': 'true'}), token=token)
        user_id = users[0]['id']
        request(f'/admin/realms/polleria/users/{user_id}', {
            'firstName': role.capitalize(), 'lastName': 'Prueba', 'email': username + '@polleria.test',
            'emailVerified': True, 'requiredActions': [],
        }, token, method='PUT')
        request(f'/admin/realms/polleria/users/{user_id}/reset-password', {
            'type': 'password', 'temporary': False, 'value': env['KEYCLOAK_' + role + '_PASSWORD'],
        }, token, method='PUT')
        definition = request('/admin/realms/polleria/roles/' + role, token=token)
        request(f'/admin/realms/polleria/users/{user_id}/role-mappings/realm', [definition], token)
        print(username + ': usuario de desarrollo y rol configurados')

if __name__ == '__main__':
    main()
