import pyparsing as pp
import json

# initialization of query language parsing for stackframes inspection strategy
clauseLiteral = pp.Word(pp.alphas)('field') + pp.oneOf('=')('operator') + (pp.QuotedString('"') | pp.QuotedString("'"))('value')
clauseInteger = pp.Word(pp.alphas)('field') + pp.oneOf('= < > <= >=')('operator') + pp.Word(pp.nums)('value')
clause = clauseLiteral | clauseInteger

class ClauseLiteralExpression(object):
  def __init__(self, tokens):
    self.tokens = tokens
  
  def __repr__(self):
    return str(self.asDict())
  
  def asDict(self):
    return self.tokens.asDict()

class ClauseIntegerExpression(object):
  def __init__(self, tokens):
    self.tokens = tokens
  
  def __repr__(self):
    return str(self.asDict())
  
  def asDict(self):
    return self.tokens.asDict()

clauseLiteral.setParseAction(ClauseLiteralExpression)
clauseInteger.setParseAction(ClauseIntegerExpression)

statement = pp.infixNotation(clause, [
    ('NOT', 1, pp.opAssoc.RIGHT),
    ('AND', 2, pp.opAssoc.LEFT),
    ('OR', 2, pp.opAssoc.LEFT)
])

def statementToJson(s):
  try:
    dico = statementToJsonRecurs(statement.parseString(s)[0])
    return json.dumps(dico)
  except:
    return ""

def statementToJsonRecurs(st):
  if isinstance(st, ClauseLiteralExpression):
    return {st.asDict()['field'] : st.asDict()['value']}
  if isinstance(st, ClauseIntegerExpression):
    v = int(st.asDict()['value'])
    f = ""
    if st.asDict()['operator'] == '=':
      f = st.asDict()['field']
    if st.asDict()['operator'] == '>=':
      f = st.asDict()['field'] + "Min"
    if st.asDict()['operator'] == '<=':
      f = st.asDict()['field'] + "Max"
    if st.asDict()['operator'] == '>':
      f = st.asDict()['field'] + "Min"
      v += 1
    if st.asDict()['operator'] == '<':
      f = st.asDict()['field'] + "Max"
      v -= 1
    return {f : v}
  if st[0] == "NOT":
    return {st[0]: statementToJsonRecurs(st[1])}
  if len(st) > 2 and len(st) % 2 == 1:
    return {st[1] : [statementToJsonRecurs(st[i]) for i in range(0, len(st), 2)]}
  return ""
