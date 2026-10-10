#!/usr/bin/env python3
"""Create local random credentials without overwriting existing configuration."""
from pathlib import Path
import base64,secrets
root=Path(__file__).resolve().parents[1]
target=root/'.env'
if target.exists():
 print('.env already exists; left unchanged')
else:
 names=['MYSQL_ROOT_PASSWORD','USER_DB_PASSWORD','TICKET_DB_PASSWORD','AI_DB_PASSWORD','JWT_SECRET','INTERNAL_USER_SECRET','INTERNAL_TICKET_SECRET','INTERNAL_AI_SECRET']
 content=''.join(name+'='+secrets.token_hex(24)+'\n' for name in names)
 content+='NACOS_AUTH_TOKEN='+base64.b64encode(secrets.token_bytes(48)).decode()+'\nNACOS_PASSWORD=nacos\nAI_PROFILES=demo,stub\nMAIL_STUB=true\n'
 target.write_text(content);target.chmod(0o600);print('Created .env with local credentials')
