#!/usr/bin/env python3
"""
Room SQL audit for Clock Adventure 3D.

Room validates every @Query at compile time: an unknown table or column name fails the build, and
so does a query whose projection does not match the return type. This sandbox cannot run KSP, so
this script performs the same first check statically - it maps every @Entity to a table name and a
column list, then verifies the table names and the columns used inside @Query strings.

Usage:
    python3 audit_room.py
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))
DB_DIR = os.path.join(PROJECT, "data", "src", "main", "kotlin", "com", "clockadventure", "data", "db")

SQL_KEYWORDS = {
    "select", "from", "where", "order", "by", "asc", "desc", "update", "set", "insert", "into",
    "values", "delete", "count", "sum", "avg", "max", "min", "and", "or", "not", "null", "is",
    "as", "on", "join", "left", "inner", "group", "having", "limit", "case", "when", "then",
    "else", "end", "distinct", "cast", "like", "in", "exists", "coalesce", "ifnull",
}

ENTITY_RE = re.compile(r'@Entity\(([^)]*)\)\s*data class (\w+)\s*\((.*?)\n\)', re.DOTALL)
COLUMN_INFO_RE = re.compile(r'@ColumnInfo\(name\s*=\s*"([^"]+)"\)')
FIELD_RE = re.compile(r'(?:^|\s)(?:val|var)\s+(\w+)\s*:')
QUERY_RE = re.compile(r'@Query\(\s*"([^"]+)"\s*\)')
TABLE_REF_RE = re.compile(r'\b(?:from|into|update|join)\s+([A-Za-z_][A-Za-z0-9_]*)', re.IGNORECASE)


def parse_entities(text):
    """Maps table name -> set of column names for every @Entity data class."""
    tables = {}
    for match in ENTITY_RE.finditer(text):
        attributes, class_name, body = match.group(1), match.group(2), match.group(3)
        table = re.search(r'tableName\s*=\s*"([^"]+)"', attributes)
        table_name = table.group(1) if table else class_name
        columns = set()
        for line in body.split("\n"):
            line = line.strip()
            renamed = COLUMN_INFO_RE.search(line)
            if renamed:
                columns.add(renamed.group(1))
                continue
            field = FIELD_RE.search(line)
            if field:
                columns.add(field.group(1))
        tables[table_name] = columns
    return tables


def check_queries(text, tables):
    problems = []
    for match in QUERY_RE.finditer(text):
        query = match.group(1)
        referenced_tables = {m.group(1) for m in TABLE_REF_RE.finditer(query)}
        for name in referenced_tables:
            if name not in tables:
                problems.append('  @Query uses unknown table "%s": %s' % (name, query))
        known_columns = set()
        for name in referenced_tables:
            known_columns |= tables.get(name, set())
        if not known_columns:
            continue
        # identifiers that are not table names, not bound parameters and not SQL keywords
        for identifier in re.finditer(r'(?<![:.\w])([a-z][A-Za-z0-9_]*)\b', query):
            name = identifier.group(1)
            if name in SQL_KEYWORDS or name in tables or name in known_columns:
                continue
            problems.append('  @Query uses unknown column "%s": %s' % (name, query))
    return problems


def main():
    if not os.path.isdir(DB_DIR):
        print("no database folder found")
        return 0
    text = ""
    for name in sorted(os.listdir(DB_DIR)):
        if name.endswith(".kt"):
            text += open(os.path.join(DB_DIR, name), encoding="utf-8").read() + "\n"

    tables = parse_entities(text)
    if not tables:
        print("no @Entity classes found - nothing to check")
        return 0
    print("tables: " + ", ".join("%s(%d)" % (name, len(cols)) for name, cols in sorted(tables.items())))
    problems = check_queries(text, tables)
    if problems:
        print("PROBLEMS")
        print("\n".join(dict.fromkeys(problems)))
        return 1
    print("OK - every table and column referenced in @Query exists on its entity")
    return 0


if __name__ == "__main__":
    sys.exit(main())
