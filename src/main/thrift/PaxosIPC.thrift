struct PrepareResponse {
  1: required i32 round;
  2: optional i32 value;
}

service PaxosIPC {
  PrepareResponse prepare(1:i32 round);
  i32 accept(1:i32 round, 2:i32 value);
  void decided(1:i32 value);
}
